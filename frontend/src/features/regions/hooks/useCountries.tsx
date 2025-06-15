import { useEffect, useState } from "react";
import { RegionApiRepository } from "../data/RegionApiRepository";
import ApiHelper from "../../../util/api/ApiHelper";
import { Country } from "../domain/Country";

export const useCountries = () => {
  const [countries, setCountries] = useState<Country[]>([]);

  useEffect(() => {
    (async () => {
      try {
        setCountries(
          await new RegionApiRepository(new ApiHelper()).getCountries()
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
