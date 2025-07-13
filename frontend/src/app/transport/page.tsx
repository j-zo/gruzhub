/* eslint-disable @next/next/no-sync-scripts */
"use client";
import {MechanicComponent} from "@/features/common/transport/presentation/MechanicComponent";
import {Menu} from "../../features/menu/presentation/Menu";
import {AUTHORIED_USER_ID_KEY, AUTHORIED_USER_TOKEN_KEY,} from "../../constants";
import UserApiRepository from "../../features/user/data/UserApiRepository";
import ApiHelper from "../../util/api/ApiHelper";
import {TransportComponent} from "@/features/common/transport/presentation/TransportComponent";


const apiHelper = new ApiHelper();
const userApiRepository = new UserApiRepository(
  apiHelper,
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY
);
apiHelper.attachUserRepository(userApiRepository);

export default function Home() {
  return (
    <>
      <script src="https://telegram.org/js/telegram-web-app.js" />
      <Menu userApiRepository={userApiRepository}>
          <main className="flex flex-col">
              <MechanicComponent
                  userApiRepository={userApiRepository}
              />
              <TransportComponent
                  userApiRepository={userApiRepository}
              />
          </main>
      </Menu>
    </>
  );
}
