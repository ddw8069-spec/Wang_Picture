package com.yupi.yupicturebackend.mapper;

import com.yupi.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author 李鱼皮
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2024-12-11 20:45:51
* @Entity com.yupi.yupicturebackend.model.entity.Picture
*/
public interface PictureMapper extends BaseMapper<Picture> {

    @Select("SELECT * FROM picture WHERE isDelete = 1 AND updateTime < #{threshold} LIMIT #{limit}")
    List<Picture> selectDeletedBefore(@Param("threshold") Date threshold, @Param("limit") int limit);

    @Select("SELECT * FROM picture WHERE reviewStatus = #{reviewStatus} AND isDelete = 0 AND reviewTime < #{threshold} LIMIT #{limit}")
    List<Picture> selectRejectedBefore(@Param("reviewStatus") Integer reviewStatus,
                                       @Param("threshold") Date threshold,
                                       @Param("limit") int limit);

    @Select("SELECT * FROM picture WHERE isDelete = 0 AND editTime < #{threshold} LIMIT #{limit}")
    List<Picture> selectUnmodifiedBefore(@Param("threshold") Date threshold, @Param("limit") int limit);

    @Delete("DELETE FROM picture WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
