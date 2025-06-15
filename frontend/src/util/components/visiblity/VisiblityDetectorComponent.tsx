import { useEffect } from "react";
import { HtmlUtils } from "../../HtmlUtils";

interface Props {
  onVisible(): void;
}

const DETECTOR_DIV_ID = "visiblity-detector";

export const VisiblityDetectorComponent = ({
  onVisible,
}: Props): JSX.Element => {
  useEffect(() => {
    const checkVisiblity = () => {
      if (HtmlUtils.isElementInViewport(DETECTOR_DIV_ID)) {
        onVisible();
      }
    };

    setTimeout(() => {
      checkVisiblity();
    }, 1_000);
    window.addEventListener("scroll", checkVisiblity);
    return () => {
      window.removeEventListener("scroll", checkVisiblity);
    };
  }, []);

  return (
    <div
      id={DETECTOR_DIV_ID}
      style={{
        minHeight: "1px",
        minWidth: "1px",
        backgroundColor: "transparent",
      }}
    />
  );
};
