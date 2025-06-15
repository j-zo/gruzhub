"""change_message_role

Revision ID: fd5192fca282
Revises: 3663c7ac4ba7
Create Date: 2024-11-18 12:11:52.893800

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = "fd5192fca282"
down_revision: Union[str, None] = "3663c7ac4ba7"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add a temporary column of type String
    op.add_column(
        "order_messages",
        sa.Column("user_role_temp", sa.String(), nullable=True),
    )

    # Copy and uppercase the data from 'user_role' to 'user_role_temp'
    op.execute("UPDATE order_messages SET user_role_temp = UPPER(user_role)")

    # Drop the old 'user_role' column
    op.drop_column("order_messages", "user_role")

    # Rename 'user_role_temp' to 'user_role'
    op.alter_column(
        "order_messages",
        "user_role_temp",
        new_column_name="user_role",
        existing_type=sa.String(),
    )

    # Set 'user_role' column to not nullable
    op.alter_column("order_messages", "user_role", nullable=False)


def downgrade() -> None:
    # Add a temporary column of type ARRAY(String)
    op.add_column(
        "order_messages",
        sa.Column("user_role_temp", postgresql.ARRAY(sa.String()), nullable=True),
    )

    # Copy and lowercase the data back to 'user_role_temp', converting to array
    op.execute("UPDATE order_messages SET user_role_temp = ARRAY[LOWER(user_role)]")

    # Drop the current 'user_role' column
    op.drop_column("order_messages", "user_role")

    # Rename 'user_role_temp' back to 'user_role'
    op.alter_column(
        "order_messages",
        "user_role_temp",
        new_column_name="user_role",
        existing_type=postgresql.ARRAY(sa.String()),
    )

    # Set 'user_role' column to not nullable
    op.alter_column("order_messages", "user_role", nullable=False)
