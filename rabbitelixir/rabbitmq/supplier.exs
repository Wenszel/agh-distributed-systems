defmodule Supplier do
  def handle_orders(channel) do
    receive do
      {:basic_deliver, payload, meta} ->
        IO.puts(" [ORDER] #{payload}")
        AMQP.Basic.ack(channel, meta.delivery_tag)
        AMQP.Basic.publish(channel, "confirmation_exchange", "", "confirmed:" <> payload)
        handle_orders(channel)
    end
  end

  def handle_admin(channel) do
    receive do
      {:basic_deliver, payload, meta} ->
        IO.puts(" [ADMIN] #{payload}")
        AMQP.Basic.ack(channel, meta.delivery_tag)
        handle_admin(channel)
    end
  end

  def start do
    {:ok, connection} = AMQP.Connection.open()

    spawn(fn ->
      {:ok, order_channel} = AMQP.Channel.open(connection)
      AMQP.Exchange.declare(order_channel, "order_exchange", :direct)
      AMQP.Exchange.declare(order_channel, "confirmation_exchange", :direct)

      IO.gets("Enter products the producer offers\n")
      |> String.trim()
      |> String.split([",", " "], trim: true)
      |> Enum.each(fn product ->
        AMQP.Queue.declare(order_channel, product)
        AMQP.Queue.bind(order_channel, product, "order_exchange", routing_key: product)
        AMQP.Basic.consume(order_channel, product, nil, no_ack: false)
        IO.puts("Initialized #{product} queue")
      end)

      handle_orders(order_channel)
    end)

    spawn(fn ->
      {:ok, admin_channel} = AMQP.Channel.open(connection)
      AMQP.Exchange.declare(admin_channel, "topic_admin_exchange", :topic)
      {:ok, %{queue: admin_queue}} = AMQP.Queue.declare(admin_channel, "", exclusive: true)
      AMQP.Queue.bind(admin_channel, admin_queue, "topic_admin_exchange", routing_key: "suppliers")
      AMQP.Queue.bind(admin_channel, admin_queue, "topic_admin_exchange", routing_key: "all")
      AMQP.Basic.consume(admin_channel, admin_queue, nil, no_ack: false)

      handle_admin(admin_channel)
    end)

    :timer.sleep(:infinity)
  end
end

Supplier.start()
