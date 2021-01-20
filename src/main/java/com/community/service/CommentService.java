package com.community.service;

import com.community.entity.Comment;

import java.util.List;

public interface CommentService {
    
    /**
     * 根据实体查询评论
     *
     * @param entityType 实体类型 0-帖子  1-评论
     * @param entityId   实体 id
     * @param offset     每页起始行行号
     * @param limit      一页显示多少条数据
     */
    List<Comment> selectCommentByEntity(int entityType, int entityId, int offset, int limit);

    /**
     * 根据实体查询评论数
     *
     * @param entityType 实体类型 0-帖子  1-评论
     * @param entityId   实体 id
     */
    int selectCountByEntity(int entityType, int entityId);

    /**
     * 增加评论
     */
    int insertComment(Comment comment);

}
