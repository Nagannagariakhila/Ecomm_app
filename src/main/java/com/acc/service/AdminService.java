package com.acc.service;
import java.util.List;
import com.acc.dto.AdminDTO;
import com.acc.dto.AuthResponseDTO;
import com.acc.dto.UserDTO;
import com.acc.entity.Admin;

public interface AdminService {
	AdminDTO registerAdmin(AdminDTO adminDto);
    AdminDTO getAdminById(Long id);
    List<AdminDTO> getAllAdmins();
    AdminDTO updateAdmin(Long id, AdminDTO adminDTO);
    void deleteAdmin(Long id);
	AuthResponseDTO loginAdmin(UserDTO userDto);
}
