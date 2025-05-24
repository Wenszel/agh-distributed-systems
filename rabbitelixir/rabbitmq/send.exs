defmodule Send do
  def place_order(channel, client_name) do
    product =
      IO.gets("Enter product you want to order:\n")
      |> String.trim()

    AMQP.Basic.publish(channel, "", product, {"Order for #{product}", client_name})

    place_order(channel, client_name)
  end
end

{:ok, connection} = AMQP.Connection.open()
{:ok, channel} = AMQP.Channel.open(connection)

client_name = IO.gets("Enter client name\n")
|> String.trim()
Send.place_order(channel, client_name)
