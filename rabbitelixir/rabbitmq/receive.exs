defmodule Receive do
  def wait_for_messages(channel) do
    receive do
      {:basic_deliver, payload, meta} ->
        IO.puts " [x] Received #{payload}"
        AMQP.Basic.ack(channel, meta.delivery_tag)
        wait_for_messages(channel)
    end
  end
end

{:ok, connection} = AMQP.Connection.open
{:ok, channel} = AMQP.Channel.open(connection)
AMQP.Exchange.declare(channel, "product_exchange", :direct)

IO.gets("Enter products the producer offers\n")
|> String.trim()
|> String.split(",")
|> Enum.each(fn product ->
  AMQP.Queue.declare(channel, product)
  AMQP.Queue.bind(channel, product, "product_exchange", routing_key: product)
  AMQP.Basic.consume(channel, product, nil, no_ack: false)
  IO.puts "Initialized " <> product <> " queue"
end)

Receive.wait_for_messages(channel)
