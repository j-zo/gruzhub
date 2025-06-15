import { useEffect, useState } from "react";

export const useScreenWidth = (): number | undefined => {
  const [screenWidth, setScreenWidth] = useState<number | undefined>();

  useEffect(() => {
    const updateWidth = () => {
      setScreenWidth(document.documentElement.clientWidth);
    };

    updateWidth();
    window.addEventListener("resize", updateWidth);
    return () => window.removeEventListener("resize", updateWidth);
  }, []);

  return screenWidth;
};
