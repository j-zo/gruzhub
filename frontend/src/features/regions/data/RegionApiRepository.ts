import { APPLICATION_SERVER } from "../../../constants";
import ApiHelper from "../../../util/api/ApiHelper";
import { Country } from "../domain/Country";
import { Region } from "../domain/Region";

export class RegionApiRepository {
  constructor(private readonly apiHelper: ApiHelper) {}

  async getRegions(): Promise<Region[]> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/addresses/regions`
    );
  }

  async getCountries(): Promise<Country[]> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/addresses/countries`
    );
  }
}
