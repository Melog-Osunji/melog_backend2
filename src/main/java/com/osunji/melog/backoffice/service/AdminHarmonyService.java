package com.osunji.melog.backoffice.service;

import com.osunji.melog.backoffice.dto.AdminHarmonyDeleteRequest;
import com.osunji.melog.backoffice.dto.AdminPageResponse;
import com.osunji.melog.backoffice.dto.AdminQueryInfo;
import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.harmony.entity.HarmonyRoomMembers;
import com.osunji.melog.harmony.repository.HarmonyRoomMembersRepository;
import com.osunji.melog.harmony.repository.HarmonyRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminHarmonyService {

    private final HarmonyRoomRepository harmonyRoomRepository;
    private final HarmonyRoomMembersRepository harmonyRoomMembersRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> searchHarmonyRooms(String type, String query, int page) {
        int pageIndex = Math.max(page - 1, 0);
        int size = 10;

        List<HarmonyRoomMembers> allMembers = harmonyRoomMembersRepository.findAll();

        List<Map<String, Object>> items;
        if (query != null && !query.isBlank() && type != null) {
            switch (type) {
                case "nickname" -> {
                    items = allMembers.stream()
                            .filter(m -> m.getUser().getNickname() != null &&
                                    m.getUser().getNickname().toLowerCase().contains(query.toLowerCase()))
                            .map(this::toHarmonyItem)
                            .collect(Collectors.toList());
                }
                case "harmonyRoomName" -> {
                    items = allMembers.stream()
                            .filter(m -> m.getHarmonyRoom().getName().toLowerCase().contains(query.toLowerCase()))
                            .map(this::toHarmonyItem)
                            .collect(Collectors.toList());
                }
                case "nickname_harmonyRoomName" -> {
                    items = allMembers.stream()
                            .filter(m -> (m.getUser().getNickname() != null &&
                                    m.getUser().getNickname().toLowerCase().contains(query.toLowerCase()))
                                    || m.getHarmonyRoom().getName().toLowerCase().contains(query.toLowerCase()))
                            .map(this::toHarmonyItem)
                            .collect(Collectors.toList());
                }
                default -> items = allMembers.stream().map(this::toHarmonyItem).collect(Collectors.toList());
            }
        } else {
            items = allMembers.stream().map(this::toHarmonyItem).collect(Collectors.toList());
        }

        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(pageIndex * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Map<String, Object>> pagedItems = items.subList(fromIndex, toIndex);

        AdminPageResponse pageResponse = AdminPageResponse.builder()
                .page(pageIndex + 1)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(pageIndex + 1 < totalPages)
                .hasPrevious(pageIndex > 0)
                .build();

        AdminQueryInfo queryInfo = AdminQueryInfo.builder()
                .criterion(type)
                .keyword(query)
                .build();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", pagedItems);
        result.put("page", pageResponse);
        result.put("query", queryInfo);
        return result;
    }

    private Map<String, Object> toHarmonyItem(HarmonyRoomMembers member) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("harmonyRoomId", member.getHarmonyRoom().getId().toString());
        item.put("nickname", member.getUser().getNickname());
        item.put("harmonyRoomRole", member.getRole());
        item.put("harmonyRoomName", member.getHarmonyRoom().getName());
        return item;
    }

    @Transactional
    public Map<String, Object> deleteHarmonyRoom(AdminHarmonyDeleteRequest request) {
        // Validate required fields
        List<Map<String, String>> errors = new ArrayList<>();
        if (request.getHarmonyRoomId() == null) {
            errors.add(Map.of("field", "harmonyRoomId", "reason", "하모니룸 ID는 필수 입력값입니다."));
        }
        if (request.getManagerName() == null || request.getManagerName().isBlank()) {
            errors.add(Map.of("field", "managerName", "reason", "담당자는 필수 입력값입니다."));
        }
        if (request.getDefaultContent() == null || request.getDefaultContent().isBlank()) {
            errors.add(Map.of("field", "content", "reason", "삭제 사유 내용은 필수 입력값입니다."));
        }
        if (!errors.isEmpty()) {
            throw new AdminValidationException(errors);
        }

        HarmonyRoom room = harmonyRoomRepository.findById(request.getHarmonyRoomId())
                .orElseThrow(() -> new NoSuchElementException("하모니룸을 찾을 수 없습니다."));

        String requestId = "delreq_" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> deleteRequest = new LinkedHashMap<>();
        deleteRequest.put("title", request.getDefaultTitle());
        deleteRequest.put("content", request.getDefaultContent());
        deleteRequest.put("managerName", request.getManagerName());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("harmonyRoomId", room.getId().toString());
        response.put("harmonyRoomName", room.getName());
        response.put("requestId", requestId);
        response.put("requestedAt", LocalDateTime.now().toString());
        response.put("deleteRequest", deleteRequest);

        return response;
    }

    public static class AdminValidationException extends RuntimeException {
        private final List<Map<String, String>> errors;

        public AdminValidationException(List<Map<String, String>> errors) {
            super("Validation failed");
            this.errors = errors;
        }

        public List<Map<String, String>> getErrors() {
            return errors;
        }
    }
}
