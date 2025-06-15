"use client";
import {
  AUTHORIED_USER_ID_KEY,
  AUTHORIED_USER_TOKEN_KEY,
} from "../../constants";
import { Navbar } from "../../features/navbar/Navbar";
import { OrderApiRepository } from "../../features/orders/orders/data/OrderApiRepository";
import { CreateOrderComponent } from "../../features/orders/orders/presentation/CreateOrderComponent";
import UserApiRepository from "../../features/user/data/UserApiRepository";
import ApiHelper from "../../util/api/ApiHelper";

const apiHelper = new ApiHelper();
const userApiRepository = new UserApiRepository(
  apiHelper,
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY
);
apiHelper.attachUserRepository(userApiRepository);
const orderApiRepository = new OrderApiRepository(apiHelper, userApiRepository);

export default function Page() {
  return (
    <div className="mx-3 mb-10">
      <Navbar isMdPadding={false} isShowLogin isSidebar={false} />

      <div className="flex justify-center">
        <div className="max-w-sm mt-4 sm:mt-8">
          <h1 className="text-2xl font-bold mb-5">Создать заявку</h1>

          <CreateOrderComponent
            userApiRepository={userApiRepository}
            orderApiRepository={orderApiRepository}
          />
        </div>
      </div>
    </div>
  );
}
