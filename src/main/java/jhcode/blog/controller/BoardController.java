package jhcode.blog.controller;

import jhcode.blog.service.BoardService;
import jhcode.blog.dto.request.board.BoardUpdateDto;
import jhcode.blog.dto.request.board.BoardWriteDto;
import jhcode.blog.dto.request.board.SearchData;
import jhcode.blog.dto.response.board.ResBoardDetailsDto;
import jhcode.blog.dto.response.board.ResBoardListDto;
import jhcode.blog.dto.response.board.ResBoardWriteDto;
import jhcode.blog.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jhcode.blog.security.jwt.JwtAuthenticationFilter;

@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
@Slf4j
public class BoardController {
    private final BoardService boardService;

    // 페이징 목록
    @GetMapping("/list")
    public ResponseEntity<Page<ResBoardListDto>> boardList(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ResBoardListDto> listDTO = boardService.getAllBoards(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(listDTO);
    }

    // 페이징 검색 , Get 요청 @RequestBody 사용할 수 없음
    @GetMapping("/search")
    public ResponseEntity<Page<ResBoardListDto>> search(
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam String writerName) {
        SearchData searchData = SearchData.createdSearchData(title, content, writerName);
        Page<ResBoardListDto> searchBoard = boardService.search(searchData, pageable);
        return  ResponseEntity.status(HttpStatus.OK).body(searchBoard);
    }

    @PostMapping("/write")
    public ResponseEntity<ResBoardWriteDto> write(
            @RequestBody BoardWriteDto boardDTO,
            @AuthenticationPrincipal Member member,
            @AuthenticationPrincipal UserDetails currentUser) {

        String currentUserId = currentUser.getUsername();
        if (currentUserId == null) {
            // 사용자 인증 정보가 없는 경우 처리
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        else{
            if(!currentUserId.equals(member.getEmail())){
                log.info("loginEmail: {}, BodyEail: {}", currentUserId, member.getEmail());
                log.warn("게시글 작성 요청자와 작성자가 일치하지 않습니다.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        ResBoardWriteDto saveBoardDTO = boardService.write(boardDTO, member);
        return ResponseEntity.status(HttpStatus.CREATED).body(saveBoardDTO);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<ResBoardDetailsDto> detail(@PathVariable("boardId") Long boardId) {
        ResBoardDetailsDto findBoardDTO = boardService.detail(boardId);
        return ResponseEntity.status(HttpStatus.OK).body(findBoardDTO);
    }

    // 상세보기 -> 수정
    @PatchMapping("/{boardId}/update")
    public ResponseEntity<ResBoardDetailsDto> update(
            @PathVariable Long boardId,
            @RequestBody BoardUpdateDto boardDTO,
            @AuthenticationPrincipal UserDetails currentUser) {
        String currentUserId = currentUser.getUsername();
        if (currentUserId == null) {
            // 사용자 인증 정보가 없는 경우 처리
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        else{
            if(!currentUserId.equals(boardService.detail(boardId).getWriterName())){
                log.error("게시글 수정 요청자와 작성자가 일치하지 않습니다. -中");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        ResBoardDetailsDto updateBoardDTO = boardService.update(boardId, boardDTO);
        return ResponseEntity.status(HttpStatus.OK).body(updateBoardDTO);
    }

    // 상세보기 -> 삭제
    @DeleteMapping("/{boardId}/delete")
    public ResponseEntity<Long> delete(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails currentUser) {

        String currentUserId = currentUser.getUsername();
        log.info("userID: {}, reqID: {}", currentUserId, boardService.detail(boardId).getWriterName());
        if (currentUserId == null) {
            // 사용자 인증 정보가 없는 경우 처리
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        else{
            if(!currentUserId.equals(boardService.detail(boardId).getWriterName())){
                log.error("게시글 삭제 요청자와 작성자가 일치하지 않습니다. -中");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        boardService.delete(boardId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
    /*answer
    @PostMapping("/parentSeq}/answer")*/

}
