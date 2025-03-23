from fastapi import FastAPI
from fastapi.responses import HTMLResponse
import requests
from key import ALPHAVANTAGE_KEY, CURRENCYAPI_KEY 
from datetime import datetime, timedelta

app=FastAPI( )

def calculate_investment_return(data, currency):
    investment_in_currency = data["initial_investment"] / data["exchange_rate"][currency]
    units_purchased = investment_in_currency / data["date_price"]
    current_value_in_currency = units_purchased * data["todays_price"]
    initial_value_in_pln = data["initial_investment"]
    current_value_in_pln = current_value_in_currency * data["today_exchange_rate"][currency]
    return_percentage = (current_value_in_pln - initial_value_in_pln) / initial_value_in_pln * 100
    return return_percentage

def get_stock_prices_uri(ticker: str, key: str, mock=False) -> str:
    if mock:
        return "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=000002.SHZ&outputsize=full&apikey=demo"
    else: 
        return f'https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol={ticker}&apikey={key}&outputsize=full'

def get_exchange_rate_uri(date_YYYY_MM_DD :str, currency: str, key: str) -> str:
    return f'https://api.currencyapi.com/v3/historical?apikey={key}&currencies=PLN%2CUSD%2CCHF%2CGBP%2CEUR&base_currency={currency}&date={date_YYYY_MM_DD}'

def get_today(offset: int) -> str:
    today = datetime.today().date()
    today = today - timedelta(days=offset)
    return str(today)

def get_offset_date(date: str, offset: int) -> str:
    date = datetime.strptime(date, "%Y-%m-%d").date()
    date = date - timedelta(days=offset)
    return str(date)

def extract_currency_values(data_dict):
    currency_dict = {}
    for currency, details in data_dict['data'].items():
        currency_dict[currency] = details['value']
    return currency_dict

def get_latest_price(stock_prices):
    offset = 0
    while get_today(offset) not in stock_prices["Time Series (Daily)"]:
        if offset > 7:
            return {
                "error": "No stock price found for the last 7 days"
            }
        offset += 1
    return float(stock_prices["Time Series (Daily)"][get_today(offset)]["4. close"])

def get_stock_prices(ticker):
    stock_prices_uri = get_stock_prices_uri(ticker, ALPHAVANTAGE_KEY, True)
    stock_prices_response = requests.get(stock_prices_uri)
    stock_prices = stock_prices_response.json()
    return stock_prices

def get_price_on_given_day(date, stock_prices):
    offset = 0
    while get_offset_date(date, offset) not in stock_prices["Time Series (Daily)"]:
        if offset > 7:
            return {
                "error": "No stock price found on the buy date"
            }
        offset += 1

    return float(stock_prices["Time Series (Daily)"][get_offset_date(date, offset)]["4. close"])


def get_exchange_rate(date, currency):
    exchange_rate_uri = get_exchange_rate_uri(date, currency, CURRENCYAPI_KEY)
    exchange_rate_response = requests.get(exchange_rate_uri)
    exchange_rate = exchange_rate_response.json()
    return extract_currency_values(exchange_rate) 
 
@app.get("/roi")
async def root(ticker, currency, date, amount) :
    stock_prices = get_stock_prices(ticker)

    latest_price = get_latest_price(stock_prices)
    if type(latest_price) == dict:
        return latest_price

    date_price = get_price_on_given_day(date, stock_prices)
    if type(date_price) == dict:
        return date_price

    latest_exchange_rate = get_exchange_rate(get_today(1), currency)
    date_exchange_rate = get_exchange_rate(date, currency)

    data = {
        "initial_investment": float(amount),
        "todays_price": latest_price,
        "date_price": date_price,
        "exchange_rate":  date_exchange_rate,
        "today_exchange_rate": latest_exchange_rate 
    }

    returns = {}
    for currency in ['PLN', 'USD', 'GBP', 'CHF', 'EUR']:
        investment_return = calculate_investment_return(data, currency)
        returns[currency] = float(amount) * (1+investment_return /100)

    return {
        "initial_investment": amount,
        "investment_return": returns 
    }


@app.get("/", response_class=HTMLResponse)
async def home() :
    with open('main.html', 'r') as file:
        html = file.read()
    return html