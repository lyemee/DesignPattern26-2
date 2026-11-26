package com.holub.life;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collection;

//.life 파일을 불러오기 전에 내용을 미리 확인하는 도구
// 보드 크기가 바뀌어(64x64 -> 40x40) 예전 파일의 일부 칸이 새 보드 밖에 있을 수 있으므로,
// 불러오기 전에 보드 밖에 있는 살아 있는 칸 수를 세어 사용자에게 알리는 데 사용
// (.life 파일 형식은 Neighborhood의 저장 방식 그대로: 살아 있는 칸 좌표(Point) 목록을 직렬화)
public final class LifeFileInspector
{
	private LifeFileInspector() {}

	//저장된 살아 있는 칸 중 width x width 보드 밖에 있는 칸 수
	public static int countOutside(InputStream in, int width) throws IOException
	{
		Object saved;
		try
		{	saved = new ObjectInputStream(in).readObject();
		}
		catch(ClassNotFoundException e)
		{	throw new IOException("Life 파일 형식이 아닙니다", e);
		}
		if(!(saved instanceof Collection))
			throw new IOException("Life 파일 형식이 아닙니다");

		int outside = 0;
		for(Object item : (Collection<?>) saved)
		{
			if(!(item instanceof Point))
				throw new IOException("Life 파일 형식이 아닙니다");
			Point p = (Point) item;
			if(p.x < 0 || p.y < 0 || p.x >= width || p.y >= width)
				outside++;
		}
		return outside;
	}
}
