import random
import pytest
from faker import Faker
from sqlalchemy.ext.asyncio import AsyncSession

from src.addresses.addresses import schemas, service
from src.addresses.addresses.dependencies import AddressesDI

faker = Faker()


class TestAddressesService:
    @staticmethod
    @pytest.mark.asyncio()
    async def test_create_address(db_connection: AsyncSession):
        addresses_service = AddressesDI.get_addresses_service()
        address = TestAddressesService._generate_address_schema()
        await addresses_service.create_address(db_connection, address)

    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_address(db_connection: AsyncSession):
        addresses_service = AddressesDI.get_addresses_service()
        address_to_create = TestAddressesService._generate_address_schema()
        created_address = await addresses_service.create_address(
            db_connection,
            address_to_create,
        )

        await TestAddressesService._get_and_validate_address(
            db_connection=db_connection,
            addresses_service=addresses_service,
            address_id=created_address.id,
            schema_to_validate=address_to_create,
        )

    @staticmethod
    @pytest.mark.asyncio()
    async def test_update_address(db_connection: AsyncSession):
        addresses_service = AddressesDI.get_addresses_service()
        address_to_create = TestAddressesService._generate_address_schema()
        created_address = await addresses_service.create_address(
            db_connection,
            address_to_create,
        )

        address_to_update = TestAddressesService._generate_address_schema()
        address_to_update.id = created_address.id
        await addresses_service.update_address(db_connection, address_to_update)

        await TestAddressesService._get_and_validate_address(
            db_connection=db_connection,
            addresses_service=addresses_service,
            address_id=created_address.id,
            schema_to_validate=address_to_update,
        )

    @staticmethod
    def _generate_address_schema() -> schemas.Address:
        return schemas.Address(
            region_id=random.randint(20, 50),
            city=faker.text(),
            street=faker.text(),
            latitude=float(random.randint(0, 180)) + 0.22,
            longtitude=float(random.randint(0, 180)) + 0.11,
        )

    @staticmethod
    async def _get_and_validate_address(
        *,
        db_connection: AsyncSession,
        addresses_service: service.AddressesService,
        address_id: int,
        schema_to_validate: schemas.Address,
    ):
        address = await addresses_service.get_address_by_id(db_connection, address_id)
        assert address.region_id == schema_to_validate.region_id
        assert address.region_name
        assert address.city == schema_to_validate.city
        assert address.street == schema_to_validate.street
        assert address.latitude == schema_to_validate.latitude
        assert address.longtitude == schema_to_validate.longtitude
