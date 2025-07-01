import { v4 as uuidv4 } from "uuid";
import { APPLICATION_SERVER } from "../../../../constants";
import ApiHelper from "../../../../util/api/ApiHelper";
import RequestOptions from "../../../../util/api/RequestOptions";
import UserApiRepository from "../../../user/data/UserApiRepository";
import { OrderMessage } from "../domain/OrderMessage";
import { User } from "../../../user/domain/User";

export class OrderMessageApiRepository {
  constructor(
    private readonly apiHelper: ApiHelper,
    private readonly userApiRepository: UserApiRepository
  ) {}

  async sendMessage(orderId: number, text: string) {
    const requestOptions = new RequestOptions();
    requestOptions.setBody(
      JSON.stringify({
        guaranteeId: `${Date.now()}-${orderId}-${uuidv4()}`,
        orderId: orderId,
        text: text,
      })
    );

    return this.apiHelper.fetchPostRaw(
      `${APPLICATION_SERVER}/api/orders/messages/send`,
        this.userApiRepository,
      requestOptions
    );
  }

  async sendFileMessage(
    orderId: number,
    filename: string,
    extension: string,
    file: File
  ) {
    const guaranteeId = `${Date.now()}-${orderId}-${uuidv4()}`;

    const formData = new FormData();
    formData.append("file", file);

    const requestOptions = new RequestOptions();
    requestOptions.setFormData(formData);

    return this.apiHelper.fetchPostMultipart(
      `${APPLICATION_SERVER}/api/orders/messages/send-file?guaranteeId=${encodeURI(
        guaranteeId
      )}&orderId=${orderId}&filename=${filename}&extension=${extension}`,
        this.userApiRepository,
      requestOptions,
    );
  }

  async getLastMessagesPerEachOrder(
    ordersIds: number[]
  ): Promise<OrderMessage[]> {
    const requestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify({ ordersIds: ordersIds }));

    return this.apiHelper.fetchPostJson(
      `${APPLICATION_SERVER}/api/orders/messages/last-messages-per-order`,
        this.userApiRepository,
      requestOptions
    );
  }

  async getOrderMessages(orderId: number): Promise<OrderMessage[]> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders/messages/get-order-messages/${orderId}`,
      this.userApiRepository
    );
  }

  async getOrderMessagesUsers(orderId: number): Promise<User[]> {

    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders/messages/get-order-messages-users/${orderId}`,
      this.userApiRepository
    );
  }

  async setMessagesViewedByCurrentUserRole(orderId: number) {

    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/orders/messages/set-messages-viewed-by-role/${orderId}`,
      this.userApiRepository
    );
  }
}
