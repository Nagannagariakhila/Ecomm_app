package com.acc.service;
import com.acc.dto.ProfileDTO;
import java.util.List;
public interface ProfileService {
    ProfileDTO saveProfile(ProfileDTO profileDTO);
    List<ProfileDTO> getAllProfiles();
    ProfileDTO getProfileById(Long profileId); 
    ProfileDTO updateProfile(Long id, ProfileDTO profileDTO); 
    void deleteProfile(Long id); 
}