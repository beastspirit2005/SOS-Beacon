import os
import sys
import time
import uuid
from passlib.context import CryptContext

# Add the parent directory to the sys path to import app modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.database.connection import SessionLocal
from app.database.models import User

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def seed_admin():
    db = SessionLocal()
    try:
        email = "harshit2500sharma@gmail.com"
        raw_password = "AdminBeacon@2026!"
        
        # Check if user already exists
        existing_user = db.query(User).filter(User.email == email).first()
        if existing_user:
            print(f"User {email} already exists. Updating password and role...")
            existing_user.password_hash = pwd_context.hash(raw_password)
            existing_user.role = "ADMIN"
            db.commit()
            print("Admin account updated successfully.")
            return

        # Create new admin user
        hashed_password = pwd_context.hash(raw_password)
        new_admin = User(
            id=str(uuid.uuid4()),
            email=email,
            name="Harshit Sharma",
            password_hash=hashed_password,
            role="ADMIN",
            created_at=int(time.time() * 1000),
            updated_at=int(time.time() * 1000)
        )
        db.add(new_admin)
        db.commit()
        print(f"Admin account created successfully for {email}")
        print(f"Temporary Password: {raw_password}")
        
    except Exception as e:
        print(f"Error seeding admin: {e}")
        db.rollback()
    finally:
        db.close()

if __name__ == "__main__":
    seed_admin()
