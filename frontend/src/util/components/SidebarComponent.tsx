import Image from "next/image";
import styles from "./SidebarComponent.module.css";
import { useEffect } from "react";

interface Props {
  sidebarName: string;
  onCloseSidebar(): void;
  sidebarInner: JSX.Element;
  isFullScreen?: boolean;
}

export const SidebarComponent = ({
  sidebarName,
  onCloseSidebar,
  sidebarInner,
  isFullScreen,
}: Props): JSX.Element => {
  // hide scroll on mobiles for entire window
  useEffect(() => {
    if (window.innerWidth < 1280) {
      document.body.style.overflowY = "hidden";
    }

    return () => {
      document.body.style.overflowY = "auto";
    };
  }, []);

  return (
    <div
      className={`${styles.sidebar} ${styles.sidebarOpen} shadow-2xl ${
        isFullScreen ? "" : "p-5"
      }`}
    >
      {!isFullScreen && (
        <div className="flex justify-between items-center">
          <div className="text-xl font-bold">{sidebarName}</div>

          <Image
            style={{
              cursor: "pointer",
              filter:
                "invert(21%) sepia(89%) saturate(4349%) hue-rotate(226deg) brightness(89%) contrast(90%)",
            }}
            onClick={() => onCloseSidebar()}
            width={30}
            height={30}
            src="/icons/close.svg"
            alt="close"
          />
        </div>
      )}

      <div className={isFullScreen ? "" : "mt-5"}>{sidebarInner}</div>
    </div>
  );
};
