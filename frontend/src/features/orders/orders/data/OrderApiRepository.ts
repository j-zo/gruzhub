import { APPLICATION_SERVER } from "../../../../constants";
import ApiHelper from "../../../../util/api/ApiHelper";
import RequestOptions from "../../../../util/api/RequestOptions";
import UserApiRepository from "../../../user/data/UserApiRepository";
import { Order } from "../domain/Order";
import { OrderStatusChange } from "../domain/OrderStatusChange";
import { UserInfoChange } from "../domain/UserInfoChange";
import { CreateOrderRequest } from "../domain/dto/CreateOrderRequest";
import { CreateOrderResponse } from "../domain/dto/CreateOrderResponse";

export class OrderApiRepository {
  constructor(
    private readonly apiHelper: ApiHelper,
    private readonly userApiRepository: UserApiRepository
  ) {}

  async postOrder(order: CreateOrderRequest): Promise<CreateOrderResponse> {
    const requestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify(order));

    if (this.userApiRepository.isAuthorized()) {
      requestOptions.addHeader(
        "Authorization",
        this.userApiRepository.getAuthorizedData().accessToken
      );
    }

    return this.apiHelper.fetchPostJson(
      `${APPLICATION_SERVER}/api/orders/create`,
      requestOptions
    );
  }

  async getOrders(
    regionsIds: number[] | undefined,
    statuses: string[] | undefined,
    userId: number | undefined,
    ordersLimit: number
  ): Promise<Order[]> {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );
    requestOptions.setBody(
      JSON.stringify({
        statuses,
        regionsIds: regionsIds,
        userId: userId,
        limit: ordersLimit,
      })
    );

    return this.apiHelper.fetchPostJson(
      `${APPLICATION_SERVER}/api/orders/orders`,
      requestOptions
    );
  }

  async getOrder(orderId: number): Promise<Order> {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders/${orderId}`,
      requestOptions
    );
  }

  async getUserInfoChanges(
    userId: number,
    orderId: number
  ): Promise<UserInfoChange[]> {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders/user-changes?userId=${userId}&orderId=${orderId}`,
      requestOptions
    );
  }

  async startCalculationByMaster(orderId: number) {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/start_calculation_by_master`,
      requestOptions
    );
  }

  async declineMasterByCustomer(orderId: number, comment: string) {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );
    requestOptions.setBody(JSON.stringify({ comment }));

    return this.apiHelper.fetchPostRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/decline_order_master`,
      requestOptions
    );
  }

  async sendForConfirmationByMaster(orderId: number) {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/send_for_confirmation_by_master`,
      requestOptions
    );
  }

  async acceptByCustomer(orderId: number) {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/accept_by_customer`,
      requestOptions
    );
  }

  async cancelOrder(orderId: number, comment: string) {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );
    requestOptions.setBody(JSON.stringify({ comment }));

    return this.apiHelper.fetchPostRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/cancel_order`,
      requestOptions
    );
  }

  async completeOrder(orderId: number) {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/complete_order`,
      requestOptions
    );
  }

  async getOrderStatusChanges(orderId: number): Promise<OrderStatusChange[]> {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders/order-status-changes/${orderId}`,
      requestOptions
    );
  }
}
