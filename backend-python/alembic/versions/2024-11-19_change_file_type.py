"""change_file_type

Revision ID: 07b7f72e7b41
Revises: b0a296c9b2a8
Create Date: 2024-11-19 19:10:05.564325

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = "07b7f72e7b41"
down_revision: Union[str, None] = "b0a296c9b2a8"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add a temporary column of type String
    op.add_column(
        "files",
        sa.Column("type_temp", sa.String(), nullable=True),
    )

    # Copy and uppercase the data from 'type' to 'type_temp'
    op.execute(
        """
        UPDATE files
        SET type_temp = UPPER(type)
        WHERE type IS NOT NULL
        """
    )

    # Drop the old 'type' column
    op.drop_column("files", "type")

    # Rename 'type_temp' to 'type'
    op.alter_column(
        "files",
        "type_temp",
        new_column_name="type",
        existing_type=sa.String(),
    )

    # Set 'type' column to not nullable
    op.alter_column("files", "type", nullable=False)


def downgrade() -> None:
    # Add a temporary column of type ARRAY(String)
    op.add_column(
        "files",
        sa.Column("type_temp", postgresql.ARRAY(sa.String()), nullable=True),
    )

    # Copy data back to the array column
    op.execute(
        """
        UPDATE files
        SET type_temp = ARRAY[LOWER(type)]
        WHERE type IS NOT NULL
        """
    )

    # Drop the current 'type' column
    op.drop_column("files", "type")

    # Rename 'type_temp' to 'type'
    op.alter_column(
        "files",
        "type_temp",
        new_column_name="type",
        existing_type=postgresql.ARRAY(sa.String()),
    )

    # Set 'type' column to not nullable
    op.alter_column("files", "type", nullable=False)
