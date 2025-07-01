import { APPLICATION_SERVER } from "../../../../constants";
import ApiHelper from "../../../../util/api/ApiHelper";
import RequestOptions from "../../../../util/api/RequestOptions";
import UserApiRepository from "../../../user/data/UserApiRepository";
import { Auto } from "../domain/Auto";

export class AutoApiRepository {
  constructor(
    private readonly apiHelper: ApiHelper,
    private readonly userApiRepository: UserApiRepository
  ) {}

  async getOrderAuto(orderId: number, autoId: number): Promise<Auto> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders/auto?orderId=${orderId}&autoId=${autoId}`,
        this.userApiRepository
    );
  }
  async updateOrderAuto(orderId: number, autoId: number, auto: Auto) {
    const requestOptions = new RequestOptions();
    requestOptions.setBody(
      JSON.stringify({
        orderId,
        autoId,
        ...auto,
      })
    );

    return this.apiHelper.fetchPostRaw(
      `${APPLICATION_SERVER}/api/orders/auto`,
        this.userApiRepository,
      requestOptions
    );
  }
}
