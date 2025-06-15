import { useEffect, useState } from "react";
import { RegionApiRepository } from "../data/RegionApiRepository";
import { Region } from "../domain/Region";
import ApiHelper from "../../../util/api/ApiHelper";

export const useRegions = (isOnlyRussia = true) => {
  const [regions, setRegions] = useState<Region[]>([]);

  useEffect(() => {
    (async () => {
      try {
        let loadedRegions = await new RegionApiRepository(
          new ApiHelper()
        ).getRegions();

        if (isOnlyRussia) {
          loadedRegions = loadedRegions.filter(
            (region) => region.country.code === "RU"
          );
        }

        setRegions(loadedRegions);
      } catch (e) {
        console.log((e as Error).message);
        alert("Ошибка загрузки регионов. Пожалуйста, перезагрузите страницу");
      }
    })();
  }, []);

  return regions;
};
