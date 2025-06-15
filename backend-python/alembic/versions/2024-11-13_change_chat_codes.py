"""change_chat_codes

Revision ID: ea4105dee101
Revises: d9736998a153
Create Date: 2024-11-13 13:08:08.575164

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = "ea4105dee101"
down_revision: Union[str, None] = "d9736998a153"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Alter column type from ARRAY to TEXT with data conversion
    op.alter_column(
        "users",
        "user_chats_codes",
        type_=sa.Text(),
        existing_type=sa.ARRAY(sa.String()),
        nullable=True,
        postgresql_using="array_to_string(user_chats_codes, ',')",
    )


def downgrade() -> None:
    # Alter column type from TEXT back to ARRAY with data conversion
    op.alter_column(
        "users",
        "user_chats_codes",
        type_=sa.ARRAY(sa.String()),
        existing_type=sa.Text(),
        nullable=True,
        postgresql_using="string_to_array(user_chats_codes, ',')",
    )
