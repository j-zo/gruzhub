/* eslint-disable @next/next/no-img-element */
interface Props {
  imageUrl: string;
  onClose(): void;
}

export const ImageDialogComponent = ({
  imageUrl,
  onClose,
}: Props): JSX.Element => {
  return (
    <div
      style={{
        position: "fixed",
        zIndex: 99999999999,
        paddingTop: "60px",
        paddingBottom: "50px",
        left: 0,
        top: 0,
        width: "100%",
        height: "100%",
        overflow: "auto",
        backgroundColor: "#000",
        cursor: "pointer",
      }}
      onClick={() => onClose()}
    >
      <span
        style={{
          position: "absolute",
          top: "15px",
          right: "35px",
          color: "#f1f1f1",
          fontSize: "40px",
          fontWeight: "bold",
          transition: "0.2s",
          userSelect: "none",
        }}
        onClick={() => onClose()}
      >
        &times;
      </span>

      <img
        src={imageUrl}
        alt="opened"
        style={{
          margin: "auto",
          display: "block",
          objectFit: "contain",
          maxWidth: "90%",
          maxHeight: "90%",
        }}
      />
    </div>
  );
};
