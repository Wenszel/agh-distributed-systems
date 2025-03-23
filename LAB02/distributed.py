from fastapi import FastAPI
import requests
from key import ALPHAVANTAGE_KEY, CURRENCYAPI_KEY 
import json

app=FastAPI( )

def get_prices_uri(ticker: str, key: str) -> str:
    return f'https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol={ticker}&apikey={key}&outputsize=full'

def get_currency_uri(date_YYYY_MM_DD: str, key: str) -> str:
    return f'https://api.currencyapi.com/v3/historical?apikey={key}&currencies=EUR%2CUSD%2CGBP%2CCHF&base_currency=PLN&date={date_YYYY_MM_DD}'

@app.get("/roi")
async def root(ticker, currency, date) :
    uri = get_prices_uri(ticker, ALPHAVANTAGE_KEY)
    response = requests.get(uri)
    data = response.json()
    return {"message" : data}

from fastapi.responses import HTMLResponse
@app.get("/", response_class=HTMLResponse)
async def home() :
    with open('main.html', 'r') as file:
        html = file.read()
    return html