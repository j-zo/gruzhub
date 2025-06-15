import { APPLICATION_SERVER } from "@/constants";
import ApiHelper from "@/util/api/ApiHelper";
import RequestOptions from "@/util/api/RequestOptions";
import { RegistrationDto } from "../domain/RegistrationDto";
import { UserStatisticsPeriod } from "../domain/UserStatisticsPeriod";
import UserApiRepository from "../../data/UserApiRepository";

export default class UserStatisticsApiRepository {
  constructor(
    private readonly apiHelper: ApiHelper,
    private readonly userApiRepository: UserApiRepository
  ) {}

  async getRegistrationsStatistics(
    period: UserStatisticsPeriod
  ): Promise<RegistrationDto[]> {
    const requestOptions: RequestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/users-statistics/registrations?period=${period}`,
      requestOptions
    );
  }
}
