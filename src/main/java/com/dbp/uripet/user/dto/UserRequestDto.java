package com.dbp.uripet.user.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data 
@Builder
public class UserRequestDto {
  @NotBlank(message = "Name is required")
  @Size(min = 2, max = 80, message = "Name must be between 2 and 80 characters")
  private String name;

  @NotBlank(message = "Phone number is required")
  @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must contain between 7 and 15 digits")
  private String phone;
}   