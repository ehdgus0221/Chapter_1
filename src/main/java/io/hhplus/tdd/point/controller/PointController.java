package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.service.PointService;
import io.hhplus.tdd.point.entity.UserPoint;
import io.hhplus.tdd.point.dto.PointRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    private final PointService pointService;


    /**
     * 특정 유저의 포인트를 조회
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable("id") long id
    ) {
        return pointService.getPoint(id);
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable("id") long id
    ) {
        return pointService.getHistories(id);
    }

    /**
     * 특정 유저의 포인트를 충전
     */

    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable("id") long id,
            @RequestBody PointRequest request
    ) {
        return pointService.charge(id, request.getAmount());
    }

    /**
     * 특정 유저의 포인트를 사용
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable("id") long id,
            @RequestBody PointRequest request
    ) {
        return pointService.use(id, request.getAmount());
    }
}
