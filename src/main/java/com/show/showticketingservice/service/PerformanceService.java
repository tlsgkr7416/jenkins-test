package com.show.showticketingservice.service;

import com.show.showticketingservice.exception.performance.*;
import com.show.showticketingservice.mapper.PerformanceMapper;
import com.show.showticketingservice.mapper.PerformanceTimeMapper;
import com.show.showticketingservice.mapper.SeatMapper;
import com.show.showticketingservice.model.criteria.PerformancePagingCriteria;
import com.show.showticketingservice.model.enumerations.ShowType;
import com.show.showticketingservice.model.performance.*;
import com.show.showticketingservice.model.venueHall.VenueHallColumnSeat;
import com.show.showticketingservice.tool.constants.CacheConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static com.show.showticketingservice.tool.constants.CacheConstant.ALL_TYPE_MAIN_PERFORMANCE_LIST_KEY;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private static final long MAX_SCHEDULE_TIME = 24_00_00;

    private final PerformanceMapper performanceMapper;

    private final PerformanceTimeMapper performanceTimeMapper;

    private final VenueHallService venueHallService;

    private final FileService fileService;

    private final SeatPriceService seatPriceService;

    private final SeatMapper seatMapper;

    @Transactional
    public void insertPerformance(PerformanceRequest performanceRequest) {
        checkPerformanceExists(performanceRequest.getTitle(), performanceRequest.getShowType());
        performanceMapper.insertPerformance(performanceRequest);
    }

    public void checkPerformanceExists(String title, ShowType showType) {
        if (performanceMapper.isPerformanceExists(title, showType)) {
            throw new PerformanceAlreadyExistsException();
        }
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConstant.PERFORMANCE, key = "#performanceId")
    public void updatePosterImage(int performanceId, MultipartFile image) {

        fileService.checkFileContentType(image);

        String imagePath = performanceMapper.getImagePath(performanceId);

        if (imagePath != null) {
            fileService.deleteFile(imagePath);
        }

        imagePath = fileService.saveFile(image);
        performanceMapper.updatePerfImagePath(performanceId, imagePath);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConstant.PERFORMANCE, key = "#performanceId"),
            @CacheEvict(cacheNames = CacheConstant.PERFORMANCE_TIME, key = "#performanceId")
    })
    public void insertPerformanceTimes(List<PerformanceTimeRequest> performanceTimeRequests, int performanceId) {

        checkPerfTimeRequestConflict(performanceTimeRequests);

        checkPerfTimeWithDB(performanceTimeRequests, performanceId);

        performanceTimeMapper.insertPerformanceTimes(performanceTimeRequests, performanceId);

        insertSeatInfo(performanceId, performanceTimeRequests);
    }

    private void checkPerfTimeWithDB(List<PerformanceTimeRequest> performanceTimeRequests, int performanceId) {
        /*
        ??????????????? ?????? ???????????? ?????? DB??? ???????????? ?????? ???????????? ??????

        Logic:
        1. DB??? ???????????? ????????? startTime ???????????? ?????? (perfTimesFromDB)
        2. ????????? perfTimesFromDB??? endTime ???????????? '????????????' ?????? ??????????????? ???????????? ?????? ?????? ?????? ??? ????????? ??? ?????? ?????? ?????? ??????
        3. ????????? ???????????? ??????????????? ???????????? ????????? ??????????????? ????????????
         */

        List<PerformanceTimeRequest> perfTimesFromDB = performanceTimeMapper.getPerfTimes(performanceId, performanceTimeRequests);

        if (perfTimesFromDB.size() == 0)
            return;

        Collections.sort(perfTimesFromDB, Comparator.comparing(PerformanceTimeRequest::getStartTime));

        int timeRequestsSize = performanceTimeRequests.size();

        for (int idx = 0; idx < timeRequestsSize; idx++) {

            PerformanceTimeRequest timeRequest = performanceTimeRequests.get(idx);

            long startTime = Long.parseLong(timeRequest.getStartTime());
            long endTime = Long.parseLong(timeRequest.getEndTime());

            int first = 0;
            int last = perfTimesFromDB.size() - 1;

            while (first < last) {
                int mid = (first + last) / 2;

                if (Long.parseLong(perfTimesFromDB.get(mid).getEndTime()) <= startTime)
                    first = mid + 1;
                else
                    last = mid;
            }

            PerformanceTimeRequest refTimeRequest = perfTimesFromDB.get(first);

            long refStartTime = Long.parseLong(refTimeRequest.getStartTime());
            long refEndTime = Long.parseLong(refTimeRequest.getEndTime());

            if (startTime < refEndTime && refStartTime < endTime) {
                throw new PerformanceTimeConflictException("????????? ?????? ????????? ????????? ????????? ???????????? ?????? ???????????????.");
            }

        }
    }

    private void checkPerfTimeRequestConflict(List<PerformanceTimeRequest> performanceTimeRequests) {
        /*
        ?????? ????????? ?????? ????????? ????????? ?????? ??????????????? ???????????? ?????? ????????? ?????? ??? ?????? ??????
        (????????? ?????? ????????? ?????? ????????? ???????????? ?????? ??? ??????)
         */

        int size = performanceTimeRequests.size();

        Collections.sort(performanceTimeRequests, Comparator.comparing(PerformanceTimeRequest::getStartTime));

        PerformanceTimeRequest perfTime;

        long preEndTime = 0;

        for (int idx = 0; idx < size; idx++) {

            perfTime = performanceTimeRequests.get(idx);

            checkCorrectPerfTime(perfTime);

            if (idx > 0 && preEndTime >= Long.parseLong(perfTime.getStartTime())) {
                throw new PerformanceTimeConflictException("????????? ?????? ????????? ????????? ?????? ???????????????.");
            }

            preEndTime = Long.parseLong(perfTime.getEndTime());
        }
    }

    private void checkCorrectPerfTime(PerformanceTimeRequest timeRequest) {
        /*
        ???????????? ?????? ?????? ??????
        - ?????? ????????? ????????? ?????? ?????? ?????? ?????? ????????????
        - ?????? ????????? 24?????? ????????? ?????? ????????????
         */

        long startTime = Long.parseLong(timeRequest.getStartTime());
        long endTime = Long.parseLong(timeRequest.getEndTime());

        if (startTime >= endTime) {
            throw new PerformanceTimeConflictException("?????? ????????? ????????? ?????? ?????????????????????.");
        }

        if (endTime - startTime > MAX_SCHEDULE_TIME) {
            throw new PerformanceTimeConflictException("?????? ????????? 24????????? ????????? ??? ????????????.");
        }

    }

    private List<SeatRequest> setSeatInfo(int performanceId, List<PerformanceTimeRequest> performanceTimeRequests) {
        /*
        ?????? ?????? ??????
        - DB????????? ?????? ????????? ????????? ????????? ????????? (VenueHallColumnSeat, SeatPriceRowNumData)
         */

        VenueHallColumnSeat venueHallColumnSeat = venueHallService.getVenueHallColumnAndId(performanceId);

        List<SeatPriceRowNumData> seatPriceRowNumDataList = seatPriceService.getSeatPriceRowNum(performanceId);
        List<SeatRequest> seatRequests = new ArrayList<>();

        performanceTimeRequests.stream().forEach(performanceTimeRequest -> {

            seatPriceRowNumDataList.stream().forEach(seatPriceRowNumData -> {

                IntStream.range(seatPriceRowNumData.getStartRowNum(), seatPriceRowNumData.getEndRowNum() + 1).forEach(rowNum -> {

                    IntStream.range(1, venueHallColumnSeat.getColumnSeats() + 1).forEach(colNum -> {

                        SeatRequest seatRequest = SeatRequest.builder().
                                perfTimeId(performanceTimeRequest.getId()).
                                hallId(venueHallColumnSeat.getId()).
                                priceId(seatPriceRowNumData.getId()).
                                colNum(colNum).
                                rowNum(rowNum).
                                reserved(false).
                                build();

                        seatRequests.add(seatRequest);
                    });
                });
            });
        });

        return seatRequests;
    }

    public void insertSeatInfo(int performanceId, List<PerformanceTimeRequest> performanceTimeRequests) {
        seatPriceService.checkSeatPriceNotExistsException(performanceId);
        List<SeatRequest> seatRequests = setSeatInfo(performanceId, performanceTimeRequests);
        seatMapper.insertSeatInfo(seatRequests);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConstant.PERFORMANCE, key = "#performanceId")
    public void updatePerformanceInfo(int performanceId, PerformanceUpdateRequest perfUpdateRequest) {

        checkValidPerformanceId(performanceId);

        checkPerfTitleDuplicated(performanceId, perfUpdateRequest.getTitle(), perfUpdateRequest.getShowType());

        performanceMapper.updatePerformanceInfo(performanceId, perfUpdateRequest);
    }

    public void checkValidPerformanceId(int performanceId) {
        if (!performanceMapper.isPerformanceIdExists(performanceId)) {
            throw new PerformanceNotExistsException();
        }
    }

    private void checkPerfTitleDuplicated(int performanceId, String title, ShowType showType) {
        if (performanceMapper.isPerfTitleDuplicated(performanceId, title, showType)) {
            throw new PerformanceAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConstant.PERFORMANCE, key = "#performanceId")
    public PerformanceDetailInfoResponse getPerformanceDetailInfo(int performanceId) {
        checkValidPerformanceId(performanceId);
        return performanceMapper.getPerformanceDetailInfo(performanceId);
    }

    /*
    ?????? ?????? ????????? ?????? ?????? ??????
    cacheNames
      - MAIN_PERFORMANCE_LIST : ?????? ????????? ???????????? ??? ?????? ???????????? ??????
      - PERFORMANCE_LIST : ?????? ????????? ???????????? ??? ?????? ???????????? ????????? ??????
      - ALL_TYPE_MAIN_PERFORMANCE_LIST : ?????? ????????? ????????? ???????????? ??? ?????? ???????????? ??????
      - ALL_TYPE_PERFORMANCE_LIST : ?????? ????????? ????????? ???????????? ??? ?????? ???????????? ????????? ??????
     */
    @Transactional
    @Caching(cacheable = {
            @Cacheable(
                    cacheNames = CacheConstant.MAIN_PERFORMANCE_LIST,
                    condition = "#showType != null && #lastPerfId == null",
                    key = "#showType.toString()"
            ),
            @Cacheable(
                    cacheNames = CacheConstant.PERFORMANCE_LIST,
                    condition = "#showType != null && #lastPerfId != null",
                    key = "#showType.toString() + #lastPerfId"
            ),
            @Cacheable(
                    cacheNames = CacheConstant.ALL_TYPE_MAIN_PERFORMANCE_LIST,
                    condition = "#showType == null && #lastPerfId == null",
                    key = ALL_TYPE_MAIN_PERFORMANCE_LIST_KEY
            ),
            @Cacheable(
                    cacheNames = CacheConstant.ALL_TYPE_PERFORMANCE_LIST,
                    condition = "#showType == null && #lastPerfId != null",
                    key = "#lastPerfId"
            )
    })
    public List<PerformanceResponse> getPerformances(ShowType showType, PerformancePagingCriteria performancePagingCriteria) {
        checkValidPerfIdAndShowType(showType, performancePagingCriteria.getLastPerfId());
        return performanceMapper.getPerformances(showType, performancePagingCriteria);
    }

    private void checkValidPerfIdAndShowType(ShowType showType, Integer lastPerfId) {
        if (!performanceMapper.isPerfIdAndShowTypeExists(showType, lastPerfId)) {
            throw new PerformanceNotExistsException();
        }
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConstant.PERFORMANCE, key = "#performanceId"),
            @CacheEvict(cacheNames = CacheConstant.PERFORMANCE_TIME, key = "#performanceId")
    })
    public void deletePerformanceTimes(int performanceId, List<Integer> timeIds) {
        checkValidPerformanceId(performanceId);
        performanceTimeMapper.deletePerformanceTimes(performanceId, timeIds);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConstant.PERFORMANCE, key = "#performanceId"),
            @CacheEvict(cacheNames = CacheConstant.PERFORMANCE_TIME, key = "#performanceId")
    })
    public void deletePerformance(int performanceId) {
        checkValidPerformanceId(performanceId);
        performanceMapper.deletePerformance(performanceId);
    }

    @Cacheable(cacheNames = CacheConstant.PERFORMANCE_TIME, key = "#performanceId")
    public PerformanceTitleAndTimesResponse getPerformanceTitleAndTimes(int performanceId) {
        checkValidPerformanceId(performanceId);
        return performanceMapper.getPerformanceTitleAndTimes(performanceId);
    }

    @Cacheable(cacheNames = CacheConstant.PERFORMANCE_SEAT, key = "#performanceId + #perfTimeId")
    public List<PerfTimeAndRemainingSeatsResponse> getPerfTimeAndRemainingSeats(int performanceId, int perfTimeId) {
        checkValidPerformanceId(performanceId);
        checkPerfDateExists(performanceId, perfTimeId);
        return performanceTimeMapper.getPerfTimeAndRemainingSeats(performanceId, perfTimeId);
    }

    public void checkPerfDateExists(int performanceId, int perfTimeId) {
        if(!performanceTimeMapper.isPerfDateExists(performanceId, perfTimeId)) {
            throw new PerformanceTimeNotExistsException("?????? ????????? ???????????? ????????????.");
        }
    }

    public void checkPerfTimeIdExists(int perfTimeId) {
        if (!performanceTimeMapper.isPerfTimeIdExists(perfTimeId)) {
            throw new PerformanceTimeNotExistsException("?????? ?????? id??? ???????????? ????????????.");
        }
    }

    public List<PerformanceResponse> getPickedPerformances(int userId, ShowType showType, PerformancePagingCriteria performancePagingCriteria) {
        return performanceMapper.getPickedPerformances(userId, showType, performancePagingCriteria);
    }

    public List<PerformanceResponse> getPerformancesByKeyword(String keyword, PerformancePagingCriteria performancePagingCriteria) {
        if (keyword == null || keyword.isBlank())
            throw new NoKeywordException();

        return performanceMapper.getPerformancesByKeyword(keyword, performancePagingCriteria);
    }
}
