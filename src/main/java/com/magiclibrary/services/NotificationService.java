package com.magiclibrary.services;

import com.magiclibrary.dto.notification.NotificationRequestDTO;
import com.magiclibrary.dto.notification.NotificationResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NotificationService {

    List<NotificationResponseDTO> getNotificationsForUser(Integer idUser);

    Page<NotificationResponseDTO> getNotificationsForUserPaged(Integer idUser, int page, int size);

    Page<NotificationResponseDTO> getAllNotificationsPaged(int page, int size);

    NotificationResponseDTO createNotification(NotificationRequestDTO requestDTO, Integer idRequester);

    NotificationResponseDTO createSystemNotification(NotificationRequestDTO requestDTO);

    NotificationResponseDTO markAsRead(Integer idNotification, Integer idRequester);
}