from src.addresses.regions import service

regions_service = service.RegionsService()


class RegionsDI:
    @staticmethod
    def get_regions_service() -> service.RegionsService:
        return regions_service
