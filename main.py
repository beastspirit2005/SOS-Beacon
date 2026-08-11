import sys
import os
from dotenv import load_dotenv

# Load backend environment variables from the nested backend/ folder
load_dotenv(os.path.join(os.path.dirname(os.path.abspath(__file__)), "backend", ".env"))

# Add the backend directory to Python path so internal imports within the 'app' module work properly
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "backend"))

# Import the FastAPI application from backend.main to be served by the uvicorn runner at root
from backend.main import app
