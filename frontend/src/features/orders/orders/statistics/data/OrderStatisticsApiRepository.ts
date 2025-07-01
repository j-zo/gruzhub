import { APPLICATION_SERVER } from "@/constants";
import ApiHelper from "@/util/api/ApiHelper";
import RequestOptions from "@/util/api/RequestOptions";
import UserApiRepository from "@/features/user/data/UserApiRepository";
import { OrderCreationDto } from "../domain/OrderCreationDto";
import { UserStatisticsPeriod } from "@/features/user/statistics/domain/UserStatisticsPeriod";

export default class OrderStatisticsApiRepository {
  constructor(
    private readonly apiHelper: ApiHelper,
    private readonly userApiRepository: UserApiRepository
  ) {}

  async getOrdersStatistics(
    period: UserStatisticsPeriod
  ): Promise<OrderCreationDto[]> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders-statistics/orders?period=${period}`,
      this.userApiRepository
    );
  }
}
