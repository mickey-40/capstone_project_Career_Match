from dotenv import load_dotenv
import os

load_dotenv()  # Loads .env file

SECRET_KEY = os.getenv("SECRET_KEY")
DATABASE_URL = os.getenv("DATABASE_URL")
DEBUG = os.getenv("DEBUG", "False") == "True"
