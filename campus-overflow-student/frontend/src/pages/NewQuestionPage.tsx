import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { QuestionsService } from '../api/generated/services/QuestionsService';

export default function NewQuestionPage() {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      // 调用真实的发帖接口
      await QuestionsService.postV1Questions({
        title,
        body,
        courseId: 1, // 随便写一个课程 ID，具体看后端校验
        tags: [],
      });
      alert('发布成功！');
      navigate('/questions'); // 成功后跳回列表页
    } catch (error) {
      console.error(error);
      alert('发布失败！如果报 401，说明未登录被后端拦截了，这是正常的。');
    }
  };

  return (
    <div>
      <h2>发布问题</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>标题：</label>
          <input value={title} onChange={(e) => setTitle(e.target.value)} required />
        </div>
        <div>
          <label>内容：</label>
          <textarea value={body} onChange={(e) => setBody(e.target.value)} required />
        </div>
        <button type="submit">提交</button>
      </form>
    </div>
  );
}
