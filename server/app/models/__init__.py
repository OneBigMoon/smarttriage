"""SQLAlchemy models — PostgreSQL schema.

Design decisions:
- Array fields (departmentid, queue, etc.) use JSONB for simplicity.
  These are small arrays (<20 elements) used in read-heavy queries.
- Organization uses integer PK (matching old MongoDB _id).
- Box.datasource / Box.template / Box.org are string FKs (UUID or integer).
- All tables use SERIAL PK by default unless overridden.
"""
from app.models.base import Base
from app.models.box import Box
from app.models.datasource import Datasource
from app.models.user import User
from app.models.organization import Organization
from app.models.system_config import SystemConfig
from app.models.upgrade import Upgrade
from app.models.log_record import LogRecord
from app.models.template import Template
from app.models.style import Style
from app.models.datasource_type import DatasourceType

ALL_MODELS = [
    Box, Datasource, User, Organization, SystemConfig,
    Upgrade, LogRecord, Template, Style, DatasourceType,
]
