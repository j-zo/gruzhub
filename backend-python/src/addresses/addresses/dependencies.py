from src.addresses.regions.dependencies import RegionsDI
from . import service


class AddressesDI:
    @staticmethod
    def get_addresses_service() -> service.AddressesService:
        return service.AddressesService(RegionsDI.get_regions_service())
