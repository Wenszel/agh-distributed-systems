defmodule Admin do
  def wait_for_messages(channel) do
    receive do
      {:basic_deliver, payload, meta} ->
        IO.puts(" [x] Received #{payload}")
        AMQP.Basic.ack(channel, meta.delivery_tag)
        wait_for_messages(channel)
    end
  end

  def send_message(channel) do
    type =
      IO.gets("Enter type of message [clients, suppliers, all]:\n")
      |> String.trim()

    message =
      IO.gets("Enter message: \n")
      |> String.trim()

    AMQP.Basic.publish(channel, "topic_admin_exchange", type, message)
    send_message(channel)
  end

  def start do
    {:ok, connection} = AMQP.Connection.open()

    spawn(fn ->
      {:ok, channel} = AMQP.Channel.open(connection)
      AMQP.Exchange.declare(channel, "fanout_admin_exchange", :fanout)
      {:ok, %{queue: queue_name}} = AMQP.Queue.declare(channel, "", exclusive: true)
      AMQP.Queue.bind(channel, queue_name, "fanout_admin_exchange")
      AMQP.Basic.consume(channel, queue_name, nil, no_ack: false)
      wait_for_messages(channel)
    end)

    spawn(fn ->
      {:ok, channel} = AMQP.Channel.open(connection)
      AMQP.Exchange.declare(channel, "topic_admin_exchange", :topic)
      send_message(channel)
    end)
    :timer.sleep(:infinity)
  end
end

Admin.start()
