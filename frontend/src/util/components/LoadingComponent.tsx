import styles from "./LoadingComponent.module.css";

interface LoadingComponentProps {
  size?: "xs" | "sm" | "md" | "lg" | "xl";
  marginX?: 0 | 1 | 2 | 3 | 4 | 5;
  color?: "primary" | "light";
  isTransparentBackground?: boolean;
}

const getSizeInRem = (size: "xs" | "sm" | "md" | "lg" | "xl"): number => {
  let sizeRem = 1;

  switch (size) {
    case "xs":
      sizeRem = 1;
      break;
    case "sm":
      sizeRem = 2;
      break;
    case "md":
      sizeRem = 4;
      break;
    case "lg":
      sizeRem = 6;
      break;
    case "xl":
      sizeRem = 8;
      break;
    default:
      break;
  }

  return sizeRem;
};

const LoadingComponent = ({
  size = "sm",
  marginX = 0,
  color = "primary",
  isTransparentBackground = false,
}: LoadingComponentProps): JSX.Element => {
  const sizeInRem = getSizeInRem(size);

  return (
    <div
      className={`mx-${marginX} ${styles.loader}`}
      style={{
        height: `${sizeInRem}rem`,
        width: `${sizeInRem}rem`,
        minHeight: `${sizeInRem}rem`,
        minWidth: `${sizeInRem}rem`,
        border: `${`${sizeInRem / 9}rem solid ${
          color === "primary" ? "#2563ed" : "#fff"
        }`}`,
        borderTop: `${`${sizeInRem / 9}rem solid ${
          color === "primary"
            ? isTransparentBackground
              ? "transparent"
              : "#fff"
            : "#2563ed"
        }`}`,
      }}
    />
  );
};
export default LoadingComponent;
