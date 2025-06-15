package ru.gruzhub.orders.messages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.gruzhub.orders.messages.models.OrderMessage;
import ru.gruzhub.tools.files.models.File;
import ru.gruzhub.users.enums.UserRole;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMessageDto {
    private Long id;
    private String guaranteeId;
    private Long orderId;
    private Long userId;
    private UserRole userRole;
    private String text;
    private Long date;
    private File file;
    private String fileCode;
    @JsonProperty("isViewedByMaster")
    private boolean isViewedByMaster;
    @JsonProperty("isViewedByDriver")
    private boolean isViewedByDriver;
    @JsonProperty("isViewedByCustomer")
    private boolean isViewedByCustomer;

    public OrderMessageDto(OrderMessage orderMessage) {
        this.id = orderMessage.getId();
        this.guaranteeId = orderMessage.getGuaranteeId();
        this.orderId = orderMessage.getOrder().getId();
        this.userId = orderMessage.getUser().getId();
        this.userRole = orderMessage.getUserRole();
        this.text = orderMessage.getText();
        this.date = orderMessage.getDate();
        this.isViewedByMaster = orderMessage.isViewedByMaster();
        this.isViewedByDriver = orderMessage.isViewedByDriver();
        this.isViewedByCustomer = orderMessage.isViewedByCustomer();

        File file = orderMessage.getFile();
        if (file != null) {
            this.fileCode = file.getCode();
            this.file = file;
        }
    }
}
