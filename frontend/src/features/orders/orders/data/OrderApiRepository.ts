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

    return this.apiHelper.fetchPostJson(
      `${APPLICATION_SERVER}/api/orders/create`,
        this.userApiRepository,
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
        this.userApiRepository,
      requestOptions
    );
  }

  async getOrder(orderId: number): Promise<Order> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders/${orderId}`,
      this.userApiRepository
    );
  }

  async getUserInfoChanges(
    userId: number,
    orderId: number
  ): Promise<UserInfoChange[]> {
    const requestOptions = new RequestOptions();

    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders/user-changes?userId=${userId}&orderId=${orderId}`,
        this.userApiRepository,
      requestOptions
    );
  }

  async startCalculationByMaster(orderId: number) {

    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/start_calculation_by_master`,
      this.userApiRepository
    );
  }

  async declineMasterByCustomer(orderId: number, comment: string) {
    const requestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify({ comment }));

    return this.apiHelper.fetchPostRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/decline_order_master`,
        this.userApiRepository,
      requestOptions
    );
  }

  async sendForConfirmationByMaster(orderId: number) {

    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/send_for_confirmation_by_master`,
      this.userApiRepository
    );
  }

  async acceptByCustomer(orderId: number) {
    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/accept_by_customer`,
      this.userApiRepository
    );
  }

  async cancelOrder(orderId: number, comment: string) {
    const requestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify({ comment }));

    return this.apiHelper.fetchPostRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/cancel_order`,
        this.userApiRepository,
      requestOptions
    );
  }

  async completeOrder(orderId: number) {
    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/orders/${orderId}/complete_order`,
      this.userApiRepository
    );
  }

  async getOrderStatusChanges(orderId: number): Promise<OrderStatusChange[]> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/orders/order-status-changes/${orderId}`,
      this.userApiRepository
    );
  }
}
