"""change_declined_masters_ids_to_string

Revision ID: 7497faf32ecb
Revises: 44860eef9d81
Create Date: 2024-11-18 17:57:17.352184

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = "7497faf32ecb"
down_revision: Union[str, None] = "44860eef9d81"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add a temporary column of type String
    op.add_column(
        "orders",
        sa.Column("declined_masters_ids_temp", sa.String(), nullable=True),
    )

    # Copy data from the array column to the string column
    op.execute(
        """
        UPDATE orders
        SET declined_masters_ids_temp = array_to_string(declined_masters_ids, ',')
        WHERE declined_masters_ids IS NOT NULL
        """
    )

    # Drop the old array column
    op.drop_column("orders", "declined_masters_ids")

    # Rename the temporary column to the original name
    op.alter_column(
        "orders",
        "declined_masters_ids_temp",
        new_column_name="declined_masters_ids",
        existing_type=sa.String(),
    )

    # Set the column to not nullable if necessary
    op.alter_column("orders", "declined_masters_ids", nullable=False)


def downgrade() -> None:
    # Add a temporary column of type ARRAY(Integer)
    op.add_column(
        "orders",
        sa.Column(
            "declined_masters_ids_temp",
            postgresql.ARRAY(sa.Integer),
            nullable=True,
        ),
    )

    # Copy data from the string column to the array column
    op.execute(
        """
        UPDATE orders
        SET declined_masters_ids_temp = string_to_array(declined_masters_ids, ',')::integer[]
        WHERE declined_masters_ids IS NOT NULL
        """
    )

    # Drop the old string column
    op.drop_column("orders", "declined_masters_ids")

    # Rename the temporary column to the original name
    op.alter_column(
        "orders",
        "declined_masters_ids_temp",
        new_column_name="declined_masters_ids",
        existing_type=postgresql.ARRAY(sa.Integer),
    )

    # Set the column to not nullable if necessary
    op.alter_column("orders", "declined_masters_ids", nullable=False)
