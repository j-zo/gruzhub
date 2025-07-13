/* eslint-disable @next/next/no-sync-scripts */
"use client";
import { AUTHORIED_USER_TOKEN_KEY, AUTHORIED_USER_ID_KEY } from "../constants";
import { Menu } from "../features/menu/presentation/Menu";
import { OrderApiRepository } from "../features/orders/orders/data/OrderApiRepository";
import UserApiRepository from "../features/user/data/UserApiRepository";
import ApiHelper from "../util/api/ApiHelper";
import { OrdersComponent } from "../features/orders/orders/presentation/OrdersComponent";
import { TaskApiRepository } from "@/features/orders/tasks/data/TaskApiRepository";
import { TransportApiRepository } from "@/features/common/transport/data/TransportApiRepository";
import { OrderMessageApiRepository } from "../features/orders/messages/data/OrderMessageApiRepository";

const apiHelper = new ApiHelper();
const userApiRepository = new UserApiRepository(
  apiHelper,
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY
);
apiHelper.attachUserRepository(userApiRepository);
const orderApiRepository = new OrderApiRepository(apiHelper, userApiRepository);
const taskApiRepository = new TaskApiRepository(apiHelper, userApiRepository);
const autoApiRepository = new TransportApiRepository(apiHelper, userApiRepository);
const orderMessageApiRepository = new OrderMessageApiRepository(
  apiHelper,
  userApiRepository
);

export default function Home() {
  return (
    <>
      <script src="https://telegram.org/js/telegram-web-app.js" />

      <Menu userApiRepository={userApiRepository}>
        <main>
          <OrdersComponent
            orderApiRepository={orderApiRepository}
            userApiRepository={userApiRepository}
            taskApiRepository={taskApiRepository}
            autoApiRepository={autoApiRepository}
            orderMessageApiRepository={orderMessageApiRepository}
          />
        </main>
      </Menu>
    </>
  );
}
