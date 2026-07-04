"""Пароли (bcrypt), JWT-токены и refresh-токены с привязкой к устройству."""
import hashlib
import os
import secrets
from datetime import datetime, timedelta, timezone

import bcrypt
import jwt

# В проде задайте через окружение: export JWT_SECRET=$(openssl rand -hex 32)
JWT_SECRET = os.environ.get("JWT_SECRET", "CHANGE_ME_IN_PRODUCTION")
ACCESS_TTL = timedelta(minutes=15)
REFRESH_TTL = timedelta(days=30)
MAX_FAILED_ATTEMPTS = 5


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


def check_password(password: str, password_hash: str | None) -> bool:
    if not password_hash:
        return False
    return bcrypt.checkpw(password.encode(), password_hash.encode())


def make_access_token(user_id: str, role: str) -> str:
    now = datetime.now(timezone.utc)
    return jwt.encode(
        {"sub": user_id, "role": role, "iat": now, "exp": now + ACCESS_TTL},
        JWT_SECRET,
        algorithm="HS256",
    )


def decode_access_token(token: str) -> dict:
    return jwt.decode(token, JWT_SECRET, algorithms=["HS256"])


def _hash_refresh(token: str) -> str:
    # refresh-токены храним только в виде хэша — утечка базы не даст сессий
    return hashlib.sha256(token.encode()).hexdigest()


def issue_refresh_token(db, user_id: str, device_id: str) -> str:
    token = secrets.token_urlsafe(48)
    expires = (datetime.now(timezone.utc) + REFRESH_TTL).isoformat()
    # одно устройство — один живой refresh
    db.execute(
        "DELETE FROM refresh_tokens WHERE user_id=? AND device_id=?",
        (user_id, device_id),
    )
    db.execute(
        "INSERT INTO refresh_tokens(token_hash, user_id, device_id, expires_at) VALUES (?,?,?,?)",
        (_hash_refresh(token), user_id, device_id, expires),
    )
    return token


def consume_refresh_token(db, token: str, device_id: str) -> str | None:
    """Проверяет refresh, удаляет его (ротация) и возвращает user_id либо None."""
    row = db.execute(
        "SELECT user_id, device_id, expires_at FROM refresh_tokens WHERE token_hash=?",
        (_hash_refresh(token),),
    ).fetchone()
    if not row:
        return None
    db.execute("DELETE FROM refresh_tokens WHERE token_hash=?", (_hash_refresh(token),))
    if row["device_id"] != device_id:
        return None
    if datetime.fromisoformat(row["expires_at"]) < datetime.now(timezone.utc):
        return None
    return row["user_id"]


def revoke_all_tokens(db, user_id: str) -> None:
    db.execute("DELETE FROM refresh_tokens WHERE user_id=?", (user_id,))
