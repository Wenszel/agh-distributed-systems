defmodule Client do
  def place_order(channel, client_name) do
    product =
      IO.gets("Enter product you want to order:\n")
      |> String.trim()

    message = %{product: product, client: client_name} |> JSON.encode!()

    AMQP.Basic.publish(channel, "order_exchange", product, message)
    AMQP.Basic.publish(channel, "fanout_admin_exchange", "", message)

    place_order(channel, client_name)
  end

  def wait_for_messages(channel, label) do
    receive do
      {:basic_deliver, payload, meta} ->
        IO.puts(" [#{label}] Received #{payload}")
        AMQP.Basic.ack(channel, meta.delivery_tag)
        wait_for_messages(channel, label)
    end
  end

  def start do
    {:ok, connection} = AMQP.Connection.open()

    client_name =
      IO.gets("Enter client name\n")
      |> String.trim()

    spawn(fn ->
      {:ok, confirm_channel} = AMQP.Channel.open(connection)
      AMQP.Exchange.declare(confirm_channel, "confirmation_exchange", :direct)

      queue_name = client_name <> "_confirmation"
      AMQP.Queue.declare(confirm_channel, queue_name)
      AMQP.Queue.bind(confirm_channel, queue_name, "confirmation_exchange", routing_key: client_name)
      AMQP.Basic.consume(confirm_channel, queue_name, nil, no_ack: false)

      wait_for_messages(confirm_channel, "CONFIRM")
    end)

    spawn(fn ->
      {:ok, admin_channel} = AMQP.Channel.open(connection)
      AMQP.Exchange.declare(admin_channel, "fanout_admin_exchange", :fanout)
      AMQP.Exchange.declare(admin_channel, "topic_admin_exchange", :topic)

      admin_queue = client_name <> "_admin"
      AMQP.Queue.declare(admin_channel, admin_queue)
      AMQP.Queue.bind(admin_channel, admin_queue, "topic_admin_exchange", routing_key: "clients")
      AMQP.Queue.bind(admin_channel, admin_queue, "topic_admin_exchange", routing_key: "all")
      AMQP.Basic.consume(admin_channel, admin_queue, nil, no_ack: false)

      wait_for_messages(admin_channel, "ADMIN")
    end)

    spawn(fn ->
      {:ok, order_channel} = AMQP.Channel.open(connection)
      place_order(order_channel, client_name)
    end)

    :timer.sleep(:infinity)
  end
end

Client.start()
