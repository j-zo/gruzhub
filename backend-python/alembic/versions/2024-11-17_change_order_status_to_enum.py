"""change_order_status_to_enum

Revision ID: 3663c7ac4ba7
Revises: 544e7428eac2
Create Date: 2024-11-17 19:35:36.010505

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = "3663c7ac4ba7"
down_revision: Union[str, None] = "544e7428eac2"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add a temporary column of type Text
    op.add_column("orders", sa.Column("status_temp", sa.Text(), nullable=True))

    # Copy and uppercase the data from 'status' to 'status_temp'
    op.execute("UPDATE orders SET status_temp = UPPER(status)")

    # Drop the old 'status' column
    op.drop_column("orders", "status")

    # Rename 'status_temp' to 'status'
    op.alter_column("orders", "status_temp", new_column_name="status")

    # Set 'status' column to not nullable
    op.alter_column("orders", "status", nullable=False)


def downgrade() -> None:
    # Add a temporary column of type Text
    op.add_column("orders", sa.Column("status_temp", sa.Text(), nullable=True))

    # Copy and lowercase the data back to 'status_temp'
    op.execute("UPDATE orders SET status_temp = LOWER(status)")

    # Drop the current 'status' column
    op.drop_column("orders", "status")

    # Rename 'status_temp' back to 'status'
    op.alter_column("orders", "status_temp", new_column_name="status")

    # Set 'status' column to not nullable
    op.alter_column("orders", "status", nullable=False)
