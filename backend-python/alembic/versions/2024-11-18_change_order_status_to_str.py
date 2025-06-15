"""change_order_status_to_str

Revision ID: 44860eef9d81
Revises: fd5192fca282
Create Date: 2024-11-18 12:22:43.939963

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = "44860eef9d81"
down_revision: Union[str, None] = "fd5192fca282"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add a temporary column of type String
    op.add_column(
        "orders_status_changes",
        sa.Column("new_status_temp", sa.String(), nullable=True),
    )

    # Copy and uppercase the data from 'new_status' to 'new_status_temp'
    op.execute(
        """
        UPDATE orders_status_changes
        SET new_status_temp = UPPER(new_status)
        WHERE new_status IS NOT NULL
        """,
    )

    # Drop the old 'new_status' column
    op.drop_column("orders_status_changes", "new_status")

    # Rename 'new_status_temp' to 'new_status'
    op.alter_column(
        "orders_status_changes",
        "new_status_temp",
        new_column_name="new_status",
        existing_type=sa.String(),
    )

    # Set 'new_status' column to not nullable
    op.alter_column("orders_status_changes", "new_status", nullable=False)


def downgrade() -> None:
    # Add a temporary column of type ARRAY(String)
    op.add_column(
        "orders_status_changes",
        sa.Column("new_status_temp", postgresql.ARRAY(sa.String()), nullable=True),
    )

    # Copy and lowercase the data back to 'new_status_temp', converting string to array
    op.execute(
        """
        UPDATE orders_status_changes
        SET new_status_temp = ARRAY[LOWER(new_status)]
        WHERE new_status IS NOT NULL
        """,
    )

    # Drop the current 'new_status' column
    op.drop_column("orders_status_changes", "new_status")

    # Rename 'new_status_temp' back to 'new_status'
    op.alter_column(
        "orders_status_changes",
        "new_status_temp",
        new_column_name="new_status",
        existing_type=postgresql.ARRAY(sa.String()),
    )

    # Set 'new_status' column to not nullable
    op.alter_column("orders_status_changes", "new_status", nullable=False)
