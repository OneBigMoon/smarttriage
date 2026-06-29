from __future__ import annotations
"""Shared utility helpers — mirrors old lib/utils/index.js."""
import hashlib
from typing import Any


def md5(text: str | bytes) -> str:
    if isinstance(text, str):
        text = text.encode("utf-8")
    return hashlib.md5(text).hexdigest()


def pick_not_none(data: dict, keys: list[str]) -> dict:
    return {k: v for k, v in data.items() if k in keys and v is not None}


def validate_params(params: dict, fields: list[str]) -> bool:
    if not params:
        return False
    for f in fields:
        if f not in params or params[f] is None:
            return False
        v = params[f]
        if isinstance(v, str) and not v.strip():
            return False
    return True


def parse_int_list(value: Any) -> list[int]:
    if isinstance(value, list):
        return [int(x) for x in value if str(x).lstrip("-").isdigit()]
    if isinstance(value, str) and value.strip():
        return [int(x.strip()) for x in value.split(",") if x.strip().lstrip("-").isdigit()]
    return []


def parse_str_list(value: Any) -> list[str]:
    if isinstance(value, list):
        return [str(x) for x in value]
    if isinstance(value, str) and value.strip():
        return [x.strip() for x in value.split(",") if x.strip()]
    return []


def safe_int(value: Any, default: int = 0) -> int:
    """Safely convert value to int, return default on failure."""
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def version_to_sort(v: str) -> int:
    """Convert '2.4.20' → 100021000210020 (matches old system)."""
    parts = v.split(".")
    return int(f"{10000 + int(parts[0])}{10000 + int(parts[1])}{10000 + int(parts[2])}")
