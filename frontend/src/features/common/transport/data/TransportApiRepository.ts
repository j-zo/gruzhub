import { APPLICATION_SERVER } from "../../../../constants";
import ApiHelper from "../../../../util/api/ApiHelper";
import RequestOptions from "../../../../util/api/RequestOptions";
import UserApiRepository from "../../../user/data/UserApiRepository";
import { Transport } from "../domain/Transport";

export class TransportApiRepository {
  constructor(
    private readonly apiHelper: ApiHelper,
    private readonly userApiRepository: UserApiRepository
  ) {}

  async getOrderTransport(orderId: number, transportId: number): Promise<Transport> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders/transport?orderId=${orderId}&transportId=${transportId}`,
        this.userApiRepository
    );
  }
  async updateOrderTransport(orderId: number, transportId: number, transport: Transport) {
    const requestOptions = new RequestOptions();
    requestOptions.setBody(
      JSON.stringify({
        orderId,
        transportId,
        ...transport,
      })
    );

    return this.apiHelper.fetchPostRaw(
      `${APPLICATION_SERVER}/api/orders/transport`,
        this.userApiRepository,
      requestOptions
    );
  }
}
