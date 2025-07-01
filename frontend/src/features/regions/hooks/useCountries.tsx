import { useEffect, useState } from "react";
import { RegionApiRepository } from "../data/RegionApiRepository";
import ApiHelper from "../../../util/api/ApiHelper";
import { Country } from "../domain/Country";
import UserApiRepository from "@/features/user/data/UserApiRepository";

export const useCountries = () => {
  const [countries, setCountries] = useState<Country[]>([]);
  const apiHelper = new ApiHelper()
  const userApiRepository = new UserApiRepository(apiHelper, "", "")
  useEffect(() => {
    (async () => {
      try {
        setCountries(
          await new RegionApiRepository(userApiRepository, apiHelper).getCountries()
        );
      } catch (e) {
        console.log((e as Error).message);
        alert(
          "Ошибка загрузки списка стран. Пожалуйста, перезагрузите страницу"
        );
      }
    })();
  }, []);

  return countries;
};
